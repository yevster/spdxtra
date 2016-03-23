package org.quackware.spdxtra;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

public class CommandLineTool {
	public enum Operations {
		RdfXmlToJsonLd
	}

	private static final String USAGE = "spdxtra -operation [Operation] -inputFile [input file] -outputFile [output file]\n"
			+ "Valid operations are; RdfXmlToJsonLd";

	public static void main(String[] args) {
		CommandLineParser parser = new DefaultParser();

		// Create the main operation
		Options options = new Options();
		Option operationOption = new Option("operation", true, "What would you like to do?");
		operationOption.setType(Operations.class);
		operationOption.setRequired(true);
		options.addOption(operationOption);

		Option inputOption = Option.builder("inputFile").argName("inputFile").desc("The file to be processed").hasArg()
				.required().build();
		Option outputOption = Option.builder("outputFile").argName("outputFile").desc("The file to be written").hasArg()
				.required().build();
		options.addOption(inputOption);
		options.addOption(outputOption);
		try {
			CommandLine line = parser.parse(options, args);
			String operation = line.getOptionValue("operation");
			String inputFile = line.getOptionValue("inputFile");
			String outputFile = line.getOptionValue("outputFile");

			Path currentDirectory = new File(".").toPath();
			Path inputFilePath = currentDirectory.resolve(inputFile);
			if (!Files.exists(inputFilePath)) {
				System.err.println("Cannot find file " + inputFilePath.toString());
				System.exit(1);
			}
			Path outputFilePath = currentDirectory.resolve(outputFile);
			if (Files.exists(outputFilePath)) {
				System.err.println("File " + outputFilePath.toString() + " already exists.");
				System.exit(1);
			}
			
			switch (operation){
				case "RdfXmlToJsonLd":
					executeRdfXmlToJsonLd(inputFilePath, outputFilePath);
					break;
				default:
					System.out.println(USAGE);
					
			}

		} catch (ParseException pe) {
			System.err.println(USAGE);
			System.exit(1);
		}

	}

	private static void executeRdfXmlToJsonLd(Path inputPath, Path outputPath) {
		DatasetInfo datasetInfo = null;
		try {
			datasetInfo = ModelOperations.readFromFile(inputPath);
			String jsonLd = ModelOperations.toJsonLd(datasetInfo);
			FileUtils.write(outputPath.toFile(), jsonLd, Charset.forName(Charsets.UTF_16.name()));
		} catch (IOException ioe) {
			System.err.println("Unable to write file " + outputPath.toString());
			ioe.printStackTrace(System.err);
		} finally {
			FileUtils.deleteQuietly(datasetInfo.getDatasetPath().toFile());
		}
	}

}
