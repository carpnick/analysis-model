package edu.hm.hafner.analysis.parser;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.IssueParser;
import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.analysis.ReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.util.XmlElementUtil;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Parser for Taglist Maven Plugin output. During parse, class names are converted into assumed file system names, so
 * {@code package.name.class} becomes {@code package/name/class.java}.
 * 
 * @author Jason Faust
 * @see <a href= "https://www.mojohaus.org/taglist-maven-plugin/">https://www.mojohaus.org/taglist-maven-plugin/</a>
 */
public class TaglistParser extends IssueParser {
    private static final long serialVersionUID = 1L;

    @Override
    public Report parse(final ReaderFactory readerFactory) throws ParsingException {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            IssueBuilder issueBuilder = new IssueBuilder();
            Report report = new Report();

            Document document = readerFactory.readDocument();
            NodeList tags = (NodeList)xPath.evaluate("/report/tags/tag", document, XPathConstants.NODESET);
            for (Element tag : XmlElementUtil.nodeListToList(tags)) {
                String category = xPath.evaluate("@name", tag);
                issueBuilder.setCategory(category);

                NodeList files = (NodeList)xPath.evaluate("files/file", tag, XPathConstants.NODESET);
                for (Element file : XmlElementUtil.nodeListToList(files)) {
                    String clazz = xPath.evaluate("@name", file);
                    if (clazz != null) {
                        issueBuilder.setFileName(class2file(clazz));
                        issueBuilder.setPackageName(class2package(clazz));
                        issueBuilder.setAdditionalProperties(clazz);
                    }

                    NodeList comments = (NodeList)xPath.evaluate("comments/comment", file, XPathConstants.NODESET);
                    for (Element comment : XmlElementUtil.nodeListToList(comments)) {
                        issueBuilder.setLineStart(xPath.evaluate("lineNumber", comment));
                        issueBuilder.setMessage(xPath.evaluate("comment", comment));

                        report.add(issueBuilder.build());
                    }
                }
            }

            return report;
        }
        catch (XPathExpressionException e) {
            throw new ParsingException(e);
        }
    }

    private String class2file(final String clazz) {
        return clazz.replace('.', '/').concat(".java");
    }

    @Nullable
    private String class2package(final String clazz) {
        int idx = clazz.lastIndexOf('.');
        return idx > 0 ? clazz.substring(0, idx) : null;
    }

}
