package com.Jerm.service;

import com.Jerm.model.CommandResult;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShellServiceImpl implements ShellService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellServiceImpl.class);

    private static final String START_BOUNDARY = "START_BOUNDARY_" + UUID.randomUUID();
    private static final String EXIT_CODE_BOUNDARY = "EXIT_CODE_BOUNDARY_" + UUID.randomUUID();
    private static final String COMMAND_DONE_BOUNDARY = "COMMAND_DONE_BOUNDARY_" + UUID.randomUUID();

    private final PtyProcess process;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    public ShellServiceImpl() {
        try {
            String[] cmd = {"/bin/bash", "-i"};
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("TERM", "dumb");
            env.put("CLICOLOR_FORCE", "0");

            this.process = new PtyProcessBuilder(cmd).setEnvironment(env).start();
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

            consumeInitialOutput();
            LOGGER.info("PTY Shell session started successfully.");
        } catch (IOException e) {
            LOGGER.error("Failed to start PTY shell session", e);
            throw new RuntimeException("Failed to initialize shell session", e);
        }
    }

    // This helper consumes the initial shell prompt and any welcome messages
    private void consumeInitialOutput() throws IOException {
        long startTime = System.currentTimeMillis();
        // Wait for up to 2 seconds for the shell to be ready
        while (System.currentTimeMillis() - startTime < 2000) {
            if (reader.ready()) {
                char[] buffer = new char[1024];
                while(reader.ready() && reader.read(buffer) > 0) {
                    // Do nothing, just consume the output
                }
            } else {
                // Shell is quiet, assume it's ready
                return;
            }
        }
        LOGGER.warn("Shell was not quiet during startup; initialization may be slow.");
    }

    @Override
    public CommandResult executeCommand(String command) {
        if (!process.isAlive() || command.trim().equalsIgnoreCase("exit")) {
            return new CommandResult(-1, "", "Shell process is not running.");
        }
        try {
            // The Three Boundary
            String startCommand = "echo " + START_BOUNDARY;
            String exitCodeCommand = "echo " + EXIT_CODE_BOUNDARY + "$?";
            String doneCommand = "echo " + COMMAND_DONE_BOUNDARY;

            String fullCommand = startCommand + "; " + command + "; " + exitCodeCommand + "; " + doneCommand + "\n";
            writer.write(fullCommand);
            writer.flush();

            // State machine for parsing the response
            StringBuilder outputBuilder = new StringBuilder();
            int exitCode = -1;
            boolean captureStarted = false;
            String line;

            while ((line = reader.readLine()) != null) {
                // State 1: Waiting for the start boundary
                if (!captureStarted) {
                    if (line.contains(START_BOUNDARY)) {
                        captureStarted = true;
                    }
                    continue; // Discard everything before the start boundary
                }

                // State 2: Capturing output
                if (line.contains(COMMAND_DONE_BOUNDARY)) {
                    break; // End of our controlled block
                }

                if (line.contains(EXIT_CODE_BOUNDARY)) {
                    String exitCodeStr = line.substring(EXIT_CODE_BOUNDARY.length()).trim();
                    try {
                        exitCode = Integer.parseInt(exitCodeStr);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Failed to parse exit code: {}", exitCodeStr, e);
                    }
                } else {
                    // This is a real line of output from the user's command
                    outputBuilder.append(line).append("\n");
                }
            }

            return new CommandResult(exitCode, outputBuilder.toString().trim(), "");

        } catch (IOException e) {
            LOGGER.error("Error executing command: {}", command, e);
            return new CommandResult(-1, "", e.getMessage());
        }
    }

    @Override
    public void close() {
        if (process != null && process.isAlive()) {
            process.destroy();
            LOGGER.info("PTY Shell session closed.");
        }
    }
}