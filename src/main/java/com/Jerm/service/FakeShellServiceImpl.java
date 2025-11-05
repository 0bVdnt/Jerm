package com.Jerm.service;

import com.Jerm.model.CommandResult;

/**
 * A fake implementation of ShellService for UI development and testing
 * It does not execute real commands
 */
public class FakeShellServiceImpl implements ShellService {
    @Override
    public CommandResult executeCommand(String command) {
        System.out.println("FakeService: Pretending to execute '" + command + "'");
        String output = "SUCCESS: The command '" + command + "' was processed by the fake service.";
        return new CommandResult(0, output, "");
    }

    // Required to satisfy the Closeable interface contract
    @Override
    public void close() {
        System.out.println("FakeShellServiceImpl closed.");
    }
}