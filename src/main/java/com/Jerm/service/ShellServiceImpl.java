package com.Jerm.service;

import com.Jerm.model.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The real implementation of ShellService that executes commands on the underlying OS
 * It will use the Linux bash shell for now
 */
public class ShellServiceImpl implements ShellService {

    // A logger instance for this class, industry-standard logging
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellServiceImpl.class);

    @Override
    public CommandResult executeCommand(String command) {
        // ProcessBuilder, the modern and preferred way to run external processes
        // "bash -c" which tells the bash shell to execute the command string that follows
        var processBuilder = new ProcessBuilder("bash", "-c", command);

        try {
            // Start the process. This is non-blocking
            Process process = processBuilder.start();
            LOGGER.info("Executing command: '{}'", command);

            // A virtual thread executor to handle I/O streams concurrently
            // Highly efficient and prevents the process from hanging
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                // Read the standard output stream
                var outputFuture = executor.submit(() ->
                        new BufferedReader(new InputStreamReader(process.getInputStream()))
                                .lines().collect(Collectors.joining("\n")));

                // Read the standard error stream
                var errorFuture = executor.submit(() ->
                        new BufferedReader(new InputStreamReader(process.getErrorStream()))
                                .lines().collect(Collectors.joining("\n")));

                // Wait for the process to complete and get its exit code
                int exitCode = process.waitFor();
                LOGGER.info("Command finished with exit code: {}", exitCode);

                // Get the results from concurrent tasks
                String output = outputFuture.get();
                String error = errorFuture.get();

                // Return the complete result
                return new CommandResult(exitCode, output, error);
            }

        } catch (Exception e) {
            // If anything goes wrong (e.g., command not found, interrupted), log it
            LOGGER.error("Failed to execute command: {}", command, e);
            // Return an error result
            return new CommandResult(-1, "", e.getMessage());
        }
    }
}