package promptforge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class KnowledgeFrameCliTest {
    public static void main(String[] args) throws Exception {
        Path root=Files.createTempDirectory("promptforge-knowledge-frame-");
        String source=".promptforge/knowledge/adr/payment.md";
        try {
            Path cards=Files.createDirectories(root.resolve(".promptforge/knowledge/adr"));
            Files.writeString(cards.resolve("payment.md"),"# 付款約束\n\n使用同一冪等鍵。\n不可重複扣款。\n");
            Path rules=Files.createDirectories(root.resolve(".promptforge/rules"));
            Files.writeString(rules.resolve("payment.rule"),"id=payment.retry\nkeywords=付款,重試\ninstruction=先查交易狀態\n");
            Result merged=call(root,"--task","支付失敗再試","--rules","payment.retry","--knowledge",source);
            check(merged.code==0 && merged.out.startsWith("支付失敗再試") && merged.out.contains("先查交易狀態")
                    && merged.out.contains("付款約束") && merged.out.contains("使用同一冪等鍵。\n不可重複扣款。") && merged.out.contains(source),"selected full card merged with rules and sources");
            Result normalized=call(root,"--task","付款","--knowledge",source.replace('/','\\'));
            check(normalized.code==0 && normalized.out.contains(source),"Windows separators normalized");
            Result empty=call(root,"--task","支付","--rules","","--knowledge","");
            check(empty.code==0 && empty.out.strip().equals("支付"),"empty selection preserves task");
            bad(root,"--task","支付","--knowledge","../../outside.md");
            bad(root,"--task","支付","--knowledge",source+","+source);
            bad(root,"--task","支付","--knowledge",source+",");
            bad(root,"--query","支付","--knowledge",source);
            bad(root,"--validate","--knowledge",source);
            bad(root,"--list-rules","--knowledge",source);
            bad(root,"--task","支付","--knowledge",source,"--knowledge",source);
            for(int i=0;i<3;i++) Files.writeString(cards.resolve("card"+i+".md"),"# 卡片"+i+"\n\n正文"+i);
            String three=".promptforge/knowledge/adr/card0.md,.promptforge/knowledge/adr/card1.md,.promptforge/knowledge/adr/card2.md";
            Result ordered=call(root,"--task","支付","--knowledge",three);
            check(ordered.code==0 && ordered.out.indexOf("正文0")<ordered.out.indexOf("正文1") && ordered.out.indexOf("正文1")<ordered.out.indexOf("正文2"),"three selected cards preserve order");
            bad(root,"--task","支付","--knowledge",three+","+source);
            Result small=call(root,"--task","x","--knowledge",source);
            int fixed=small.out.stripTrailing().length()-1-"使用同一冪等鍵。\n不可重複扣款。".length();
            Files.writeString(cards.resolve("payment.md"),"# 付款約束\n\n"+"a".repeat(6000-fixed-1));
            Result boundary=call(root,"--task","x","--knowledge",source);
            check(boundary.code==0 && boundary.out.stripTrailing().length()==6000,"exact combined budget accepted");
            Files.writeString(cards.resolve("payment.md"),"# 付款約束\n\n"+"a".repeat(6000-fixed));
            Result over=call(root,"--task","x","--knowledge",source);
            check(over.code==5 && over.out.isEmpty(),"over budget rejected without partial prompt");
            System.out.println("PASS KnowledgeFrameCliTest");
        } finally {
            try(var paths=Files.walk(root)) { for(Path p:paths.sorted((a,b)->b.compareTo(a)).collect(java.util.stream.Collectors.toList())) Files.deleteIfExists(p); }
        }
    }
    private static void bad(Path root,String... args)throws Exception { Result r=call(root,args); check(r.code==2 && r.out.isEmpty(),"invalid knowledge selection must fail"); }
    private static Result call(Path root,String... args)throws Exception {
        String[] full=new String[args.length+2];full[0]="--project";full[1]=root.toString();System.arraycopy(args,0,full,2,args.length);
        ByteArrayOutputStream out=new ByteArrayOutputStream(),err=new ByteArrayOutputStream();
        int code=Main.run(full,new ByteArrayInputStream(new byte[0]),new PrintStream(out,true,StandardCharsets.UTF_8),new PrintStream(err,true,StandardCharsets.UTF_8));
        return new Result(code,out.toString(StandardCharsets.UTF_8));
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static final class Result {final int code;final String out;Result(int code,String out){this.code=code;this.out=out;}}
}
