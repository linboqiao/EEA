package edu.cmu.cs.lti.ark.pipeline.parsing;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import edu.cmu.cs.lti.ark.fn.data.perp.formats.TokenBuilder;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/23/15
 * Time: 11:17 PM
 */
public class MaltWrapper implements ParserImpl {

    public static final String modelName = "engmalt.linear-1.7";

    private MaltParserService malt = new MaltParserService();

    public MaltWrapper(File modelDir) throws IOException, MaltChainedException {
        malt.initializeParserModel(String.format("-w %s -c %s", modelDir.getCanonicalPath(), modelName));
    }

    public Sentence parse(Sentence input) throws ParsingException {
        List<Token> tokens = input.getTokens();

        try {
            //maltparse
            DependencyStructure outputGraph = malt.parse(
                    Iterables.toArray(Iterables.transform(tokens, new Function<Token, String>() {
                        @Nullable
                        @Override
                        public String apply(@Nullable Token token) {
                            return token.toConll();
                        }
                    }), String.class)
            );

            //convert back to a formats.Sentence
            SymbolTable deprelTable = outputGraph.getSymbolTables().getSymbolTable("DEPREL");
            String rootLabel = outputGraph.getDefaultRootEdgeLabelSymbol(deprelTable);

            List<DependencyNode> dependencyNodes = new ArrayList<DependencyNode>();

            //a little bit confusing here, so there are one more dependency node created by Malt?
            for (int tokenIndex : outputGraph.getDependencyIndices()) {
                if (tokenIndex <= tokens.size()) {
                    dependencyNodes.add(outputGraph.getDependencyNode(tokenIndex));
                }
            }

            List<Token> parsedTokens = new ArrayList<Token>();

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                DependencyNode node = dependencyNodes.get(i + 1);
                int head = node.getHead().getIndex();
                Edge arc = node.getHeadEdge();
                String deprel = arc.hasLabel(deprelTable) ? arc.getLabelSymbol(deprelTable) : rootLabel;

                Token parsedToken = TokenBuilder.aToken(token).withHead(head).withDeprel(deprel).build();

                parsedTokens.add(parsedToken);
            }

            return new Sentence(parsedTokens);
        } catch (MaltChainedException e) {
            throw new ParsingException("Malt parsing exception", e);
        }
    }
}
