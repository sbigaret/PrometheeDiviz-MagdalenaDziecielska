package org.PrometheeIRanking.xmcda;

import javafx.util.Pair;
import org.PrometheeIRanking.PrometheeIRanking;
import org.xmcda.ProgramExecutionResult;
import org.xmcda.XMCDA;
import org.xmcda.utils.ValueConverters;

import java.io.File;
import java.util.List;
import java.util.Map;

public class PrometheeIRankingCLI_XMCDAv3 {

    public static void main(String[] args) throws Utils.InvalidCommandLineException, ValueConverters.ConversionException {

        final Utils.Arguments params = Utils.parseCmdLineArguments(args);
        final String indir = params.inputDirectory;
        final String outdir = params.outputDirectory;
        final File prgExecResults = new File(outdir, "messages.xml");

        final ProgramExecutionResult executionResult = new ProgramExecutionResult();

        final XMCDA xmcda = new XMCDA();
        Utils.loadXMCDAv3(xmcda, new File(indir, "alternatives.xml"), true,
                executionResult, "alternatives");
        Utils.loadXMCDAv3(xmcda, new File(indir, "positive_flow.xml"), true,
                executionResult, "alternativesValues");
        Utils.loadXMCDAv3(xmcda, new File(indir, "negative_flow.xml"), true,
                executionResult, "alternativesValues");

        if ( ! (executionResult.isOk() || executionResult.isWarning() ) )
        {
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
        }

        final InputsHandler.Inputs inputs = InputsHandler.checkAndExtractInputs(xmcda, executionResult);

        if ( ! ( executionResult.isOk() || executionResult.isWarning() ) || inputs == null )
        {
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
        }

        final Map<Pair<String, String>, Integer> results;
        try
        {
            results = PrometheeIRanking.calculatePrometheeIRanking(inputs);
        }
        catch (Throwable t)
        {
            executionResult.addError(Utils.getMessage("The calculation could not be performed, reason: ", t));
            Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
            return;
        }

        Map<String, XMCDA> x_results = OutputsHandler.convert(inputs.alternatives_ids, results, executionResult);

        final org.xmcda.parsers.xml.xmcda_v3.XMCDAParser parser = new org.xmcda.parsers.xml.xmcda_v3.XMCDAParser();

        for ( String key : x_results.keySet() )
        {

            File outputFile = new File(outdir, String.format("%s.xml", key));

            try
            {
                parser.writeXMCDA(x_results.get(key), outputFile, OutputsHandler.xmcdaV3Tag(key));
            }
            catch (Throwable throwable)
            {
                final String err = String.format("Error while writing %s.xml, reason: ", key);
                executionResult.addError(Utils.getMessage(err, throwable));
                outputFile.delete();
            }
        }
        Utils.writeProgramExecutionResultsAndExit(prgExecResults, executionResult, Utils.XMCDA_VERSION.v3);
    }
}
