package com.Jerm.model;
/**
 * An immutable data carrier for the result of a shell command execution.
 *
 * @param exitCode The exit code of the process. 0 means success.
 * @param output   The text captured from the standard output stream.
 * @param error    The text captured from the standard error stream.
 */
public record CommandResult(int exitCode, String output, String error) {}