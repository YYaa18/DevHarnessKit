package com.devharnesskit.dhk.util;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.service.SensitiveDataGuard;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class InputUtil {
    private InputUtil() {
    }

    public static String readExclusiveText(CommandContext context, Args args,
                                           String option, String fileOption, String stdinFlag)
            throws InputException {
        return readExclusiveText(context, args, option, fileOption, stdinFlag, true);
    }

    public static String readExclusiveText(CommandContext context, Args args,
                                           String option, String fileOption, String stdinFlag,
                                           boolean redact)
            throws InputException {
        int count = 0;
        if (args.hasOption(option)) {
            count++;
        }
        if (args.hasOption(fileOption)) {
            count++;
        }
        if (args.hasFlag(stdinFlag)) {
            count++;
        }
        if (count != 1) {
            throw new InputException("Provide exactly one of --" + option
                    + ", --" + fileOption + ", or --" + stdinFlag);
        }
        if (args.hasOption(option)) {
            return maybeRedact(args.option(option), redact);
        }
        if (args.hasOption(fileOption)) {
            Path file = PathUtil.resolvePath(args.option(fileOption), context.workingDirectory());
            try {
                return maybeRedact(new String(Files.readAllBytes(file), "UTF-8"), redact);
            } catch (Exception ex) {
                throw new InputException("Failed to read --" + fileOption + ": " + ex.getMessage());
            }
        }
        return readStdin(stdinFlag, redact);
    }

    private static String readStdin(String stdinFlag, boolean redact) throws InputException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            InputStream in = System.in;
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
            }
            return maybeRedact(new String(buffer.toByteArray(), "UTF-8"), redact);
        } catch (Exception ex) {
            throw new InputException("Failed to read --" + stdinFlag + ": " + ex.getMessage());
        }
    }

    private static String maybeRedact(String value, boolean redact) {
        return redact ? new SensitiveDataGuard().redact(value) : value;
    }

    public static final class InputException extends Exception {
        public InputException(String message) {
            super(message);
        }
    }
}
