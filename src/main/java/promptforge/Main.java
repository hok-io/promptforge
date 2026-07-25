package promptforge;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** CLI boundary: parse input, load rules, compile, print only the usable result. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        PrintStream utf8Out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream utf8Err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        int code = run(args, System.in, utf8Out, utf8Err);
        if (code != 0) System.exit(code);
    }

    public static int run(String[] args, InputStream input, PrintStream output, PrintStream errors) {
        Path project = Path.of(".").toAbsolutePath().normalize();
        String task = null;
        String query = null;
        boolean stdin = false;
        boolean validate = false;
        boolean listRules = false;
        String explicitRules = null;
        String excludedRules = null;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--project":
                        if (++i >= args.length) throw new IllegalArgumentException("--project needs a path");
                        project = Path.of(args[i]).toAbsolutePath().normalize();
                        break;
                    case "--task":
                        if (++i >= args.length) throw new IllegalArgumentException("--task needs text");
                        task = args[i];
                        break;
                    case "--query":
                        if (++i >= args.length) throw new IllegalArgumentException("--query needs text");
                        query = args[i];
                        break;
                    case "--stdin":
                        stdin = true;
                        break;
                    case "--validate":
                        validate = true;
                        break;
                    case "--list-rules":
                        listRules = true;
                        break;
                    case "--rules":
                        if (explicitRules != null) throw new IllegalArgumentException("--rules may appear only once");
                        if (++i >= args.length) throw new IllegalArgumentException("--rules needs comma-separated IDs");
                        explicitRules = args[i];
                        break;
                    case "--exclude":
                        if (excludedRules != null) throw new IllegalArgumentException("--exclude may appear only once");
                        if (++i >= args.length) throw new IllegalArgumentException("--exclude needs comma-separated IDs");
                        excludedRules = args[i];
                        break;
                    default:
                        throw new IllegalArgumentException("unknown option: " + args[i]);
                }
            }
            boolean ruleOptions = explicitRules != null || excludedRules != null;
            if (listRules && (task != null || stdin || query != null || validate || ruleOptions)) {
                throw new IllegalArgumentException("use --list-rules alone");
            }
            if (ruleOptions && (query != null || validate)) {
                throw new IllegalArgumentException("rule selection requires --task or --stdin");
            }
            if (validate && (task != null || query != null || stdin)) {
                throw new IllegalArgumentException("use --validate alone");
            }
            if (query != null && (task != null || stdin)) {
                throw new IllegalArgumentException("use --query or --task/--stdin, not both");
            }
            if (!Files.isDirectory(project)) throw new IllegalArgumentException("project is not a directory: " + project);
            if (listRules) {
                List<Rule> rules = new RuleFileLoader().load(project);
                if (rules.isEmpty()) output.println("沒有專案規則。");
                for (Rule rule : rules) {
                    output.println(rule.id() + " | keywords=" + String.join(",", rule.keywords()));
                    output.println("指令：" + rule.instruction());
                    output.println("來源：" + rule.source());
                }
                return 0;
            }
            if (validate) {
                List<Rule> rules = new RuleFileLoader().load(project);
                List<KnowledgeCard> cards = new KnowledgeCardLoader().load(project);
                output.println("PASS: " + rules.size() + " rules, " + cards.size() + " knowledge cards");
                return 0;
            }
            if (query != null) {
                if (query.isBlank() || query.length() > 4000) {
                    throw new IllegalArgumentException("query must be 1-4000 characters");
                }
                List<KnowledgeCard> cards = new KnowledgeCardLoader().load(project);
                List<SearchHit> hits = new Bm25Index(cards).search(query, 3);
                if (hits.isEmpty()) {
                    output.println("沒有相關本地資料。");
                } else {
                    for (int i = 0; i < hits.size(); i++) {
                        KnowledgeCard card = hits.get(i).card();
                        String excerpt = card.body().replaceAll("\\s+", " ").trim();
                        if (excerpt.length() > 180) excerpt = excerpt.substring(0, 177) + "...";
                        output.println((i + 1) + ". " + card.title());
                        output.println("來源：" + card.source());
                        output.println("摘錄：" + excerpt);
                    }
                }
                return 0;
            }
            if (stdin && task != null) throw new IllegalArgumentException("use --task or --stdin, not both");
            if (stdin) task = new String(input.readAllBytes(), StandardCharsets.UTF_8).stripTrailing();
            if (task == null || task.isBlank()) throw new IllegalArgumentException("task must not be blank");
            List<Rule> rules = new RuleFileLoader().load(project);
            List<Rule> selected = new RuleSelection().select(task, rules, explicitRules, excludedRules);
            output.println(new TaskFrameCompiler().compileSelected(task, selected).text());
            return 0;
        } catch (FrameBudgetException e) {
            errors.println("Frame budget error: " + e.getMessage());
            return 5;
        } catch (RuleConflictException e) {
            errors.println("Rule conflict: " + e.getMessage());
            return 6;
        } catch (RuleFormatException e) {
            errors.println("Rule error: " + e.getMessage());
            return 3;
        } catch (KnowledgeFormatException e) {
            errors.println("Knowledge error: " + e.getMessage());
            return 3;
        } catch (IllegalArgumentException e) {
            errors.println("Input error: " + e.getMessage());
            return 2;
        } catch (IOException e) {
            errors.println("I/O error: " + e);
            return 4;
        }
    }
}
