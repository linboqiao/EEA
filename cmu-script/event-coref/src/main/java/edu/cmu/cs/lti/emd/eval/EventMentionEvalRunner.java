package edu.cmu.cs.lti.emd.eval;

import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 3/10/15
 * Time: 10:45 PM
 */
public class EventMentionEvalRunner {

    private double microSpanF1;

    private double microTypeF1;

    private double microRealisF1;

    private double microTypeRealisF1;

    private double microTypeAccuracy;

    private double microRealisAccuracy;

    private double macroMentionF1;

    private double macroTypeAccuracy;

    private double macroRealisAccuracy;

    public double getMicroSpanF1() {
        return microSpanF1;
    }

    public double getMicroTypeF1() {
        return microTypeF1;
    }

    public double getMicroRealisF1() {
        return microRealisF1;
    }

    public double getMicroTypeRealisF1() {
        return microTypeRealisF1;
    }

    public double getMicroTypeAccuracy() {
        return microTypeAccuracy;
    }

    public double getMicroRealisAccuracy() {
        return microRealisAccuracy;
    }

    public double getMacroMentionF1() {
        return macroMentionF1;
    }

    public double getMacroTypeAccuracy() {
        return macroTypeAccuracy;
    }

    public double getMacroRealisAccuracy() {
        return macroRealisAccuracy;
    }

    public EventMentionEvalRunner() {
    }

    public String[] buildEvalCommand(String evalScriptPath, String goldPath, String systemPath, String tokenPath,
                                     String evalOutputPath) {
        return new String[]{evalScriptPath, "-g", goldPath, "-s", systemPath, "-t", tokenPath, "-o", evalOutputPath};
    }

    public void executeShellCommand(String... command) throws IOException, InterruptedException {
        executeShellCommand(new File(System.getProperty("user.dir")), command);
    }

    public void executeShellCommand(File workingDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        pb.directory();
        pb.directory(workingDir);
        Process p = pb.start();
        p.waitFor();
    }

    public void runEval(String evalScriptPath, String goldPath, String systemPath, String tokenPath, String
            evalOutputPath) throws IOException, InterruptedException {
        String[] command = buildEvalCommand(evalScriptPath, goldPath, systemPath, tokenPath, evalOutputPath);
        System.err.println("[EXECUTE] " + Joiner.on(" ").join(command));
        executeShellCommand(command);
        getResults(evalOutputPath);
    }


    public void getResults(String evalOutputPath) throws IOException {
        List<String> lines = FileUtils.readLines(new File(evalOutputPath));

        for (int i = 0; i < 4; i++) {
            String line = lines.get(lines.size() - 1 - i).trim();
            String[] parts = line.split("\t");
            double microF1;
            if (parts[3].equals("nan")) {
                microF1 = 0;
            } else {
                microF1 = Double.parseDouble(parts[3]);
            }

            switch (parts[0]) {
                case "plain":
                    microSpanF1 = microF1;
                    break;
                case "mention_type":
                    microTypeF1 = microF1;
                    break;
                case "realis_status":
                    microRealisF1 = microF1;
                    break;
                case "mention_type+realis_status":
                    microTypeRealisF1 = microF1;
                    break;
                default:
                    break;
            }

        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        EventMentionEvalRunner runner = new EventMentionEvalRunner();

        runner.executeShellCommand("pwd");

        runner.runEval(
                "/Users/zhengzhongliu/Documents/projects/EvmEval/scorer_v1.2.py",
                "event-mention-detection/data/Event-mention-detection-2014" +
                        "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/tbf/dev_gold.tbf",
                "event-mention-detection/data/Event-mention-detection-2014" +
                        "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/tbf/dev_gold.tbf",
                "event-mention-detection/data/Event-mention-detection-2014" +
                        "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/data/token_offset",
                "event-mention-detection/data/Event-mention-detection-2014/eval_out_1.2");

        System.out.println(runner.getMicroSpanF1() + " " + runner.getMicroTypeF1());
//        System.out.flush();

    }
}