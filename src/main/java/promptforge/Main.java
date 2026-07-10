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
        int code = run(args, System.in, System.out, System.err);
        if (code != 0) System.exit(code);
    }

    public static int run(String[] args, InputStream input, PrintStream output, PrintStream errors) {
        Path project = Path.of(".").toAbsolutePath().normalize();
        String task = null;
        boolean stdin = false;
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
                    case "--stdin":
                        stdin = true;
                        break;
                    default:
                        throw new IllegalArgumentException("unknown option: " + args[i]);
                }
            }
            if (stdin && task != null) throw new IllegalArgumentException("use --task or --stdin, not both");
            if (stdin) task = new String(input.readAllBytes(), StandardCharsets.UTF_8).stripTrailing();
            if (task == null || task.isBlank()) throw new IllegalArgumentException("task must not be blank");
            if (!Files.isDirectory(project)) throw new IllegalArgumentException("project is not a directory: " + project);
            List<Rule> rules = new RuleFileLoader().load(project);
            output.println(new TaskFrameCompiler().compile(task, rules).text());
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
        } catch (IllegalArgumentException e) {
            errors.println("Input error: " + e.getMessage());
            return 2;
        } catch (IOException e) {
            errors.println("I/O error: " + e);
            return 4;
        }
    }
}
