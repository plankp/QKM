package lang.qkm;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import lang.qkm.sem.ExprChecker;
import lang.qkm.eval.*;

public class App {

    public static void main(String[] args) {
        Evaluator eval = new ASTWalker();
        eval = new ExprPrinter(eval);
        eval = new RewriteGroup(eval, List.of(
                new ANFConverter(),
                new MatchRewriter(),
                new ANFConverter(),
                new LetrecFixer(),
                new ANFConverter(),
                new PartialEvaluator(),
                new ANFConverter()));
        eval = new ExprPrinter(eval);
        final ExprChecker state = new ExprChecker(eval);

        try (final BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            StringBuilder buffer = new StringBuilder();
            String prompt = ">> ";
            String line = "";
            while (line != null) {
                System.out.print(prompt);

                prompt = "\\> ";
                line = br.readLine();
                if (line != null && !line.isEmpty()) {
                    buffer.append(line).append('\n');
                    continue;
                }
                if (buffer.isEmpty())
                    continue;

                final CharStream stream = CharStreams.fromString(buffer.toString());
                buffer = new StringBuilder();

                final CommonTokenStream tokens = new CommonTokenStream(new QKMLexer(stream));
                final QKMParser parser = new QKMParser(tokens);
                try {
                    state.visit(parser.lines());
                } catch (Exception ex) {
                    System.out.println("!! " + ex.getMessage());
                }

                prompt = ">> ";
            }
        } catch (IOException ex) {
        }
    }
}
