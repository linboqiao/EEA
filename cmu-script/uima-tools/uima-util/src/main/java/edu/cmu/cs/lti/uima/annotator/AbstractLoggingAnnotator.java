package edu.cmu.cs.lti.uima.annotator;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/7/14
 * Time: 1:45 PM
 */
public abstract class AbstractLoggingAnnotator extends AbstractAnnotator {
    public static final String PARAM_KEEP_QUIET = "keepQuiet";

    public static final String PARAM_LOGGING_OUT_FILE = "loggingOutFile";

    public static final String PARAM_ADDITIONAL_VIEWS = "targetViewNames";

    private String className = this.getClass().getName();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @ConfigurationParameter(name = PARAM_KEEP_QUIET, mandatory = false)
    private Boolean keepQuiet;

    @ConfigurationParameter(name = PARAM_LOGGING_OUT_FILE, mandatory = false)
    private String loggingFileName;

    @ConfigurationParameter(name = PARAM_ADDITIONAL_VIEWS, mandatory = false)
    private String[] targetViews;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        getFileAppender(loggingFileName);
    }

    private FileAppender<ILoggingEvent> getFileAppender(String loggingFileName) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{HH:mm:ss.SSS} [%-5level] %msg %n");
        encoder.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("file_logger");
        fileAppender.setAppend(false);
        fileAppender.setEncoder(encoder);

        fileAppender.setFile(loggingFileName);
        fileAppender.start();
        return fileAppender;
    }

    protected JCas[] getAdditionalViews(JCas mainView) {
        if (targetViews == null) {
            return new JCas[0];
        }

        JCas[] additionalViews = new JCas[targetViews.length];
        int i = 0;
        for (String viewName : targetViews) {
            additionalViews[i] = UimaConvenience.getView(mainView, viewName);
            i++;
        }
        return additionalViews;
    }

    protected void printProcessInfo(JCas aJCas, Logger logger) {
        logger.info(progressInfo(aJCas));
    }

    protected String progressInfo(JCas aJCas) {
        return "Processing " + UimaConvenience.getShortDocumentNameWithOffset(aJCas);
    }
}