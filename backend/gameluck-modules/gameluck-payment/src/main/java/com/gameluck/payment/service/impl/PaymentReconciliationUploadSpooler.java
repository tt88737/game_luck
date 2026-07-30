package com.gameluck.payment.service.impl;

import com.gameluck.common.core.exception.ServiceException;
import com.gameluck.common.core.utils.MessageUtils;
import com.gameluck.payment.service.reconciliation.PaymentReconciliationCsvParser;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import java.util.*;

@Component
public class PaymentReconciliationUploadSpooler {
    private final PaymentReconciliationSpoolCleanup cleanup;
    public PaymentReconciliationUploadSpooler(PaymentReconciliationSpoolCleanup cleanup) { this.cleanup = cleanup; }

    public Spool spool(InputStream input, long declaredSize) {
        if (input == null) throw new ServiceException(MessageUtils.message("payment.reconciliation.upload.file.required"));
        if (declaredSize > PaymentReconciliationCsvParser.MAX_BYTES) throw new ServiceException(MessageUtils.message("payment.reconciliation.upload.file.tooLarge"));
        Path directory = null, path = null; String fileId = UUID.randomUUID().toString();
        try {
            directory = createPrivateDirectory();
            path = Files.createFile(directory.resolve(fileId + ".spool"));
            secure(path, false);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size = 0; byte[] buffer = new byte[8192]; int read;
            try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING)) {
                while ((read = input.read(buffer)) != -1) {
                    size += read;
                    if (size > PaymentReconciliationCsvParser.MAX_BYTES) throw new ServiceException(MessageUtils.message("payment.reconciliation.upload.file.tooLarge"));
                    digest.update(buffer, 0, read); output.write(buffer, 0, read);
                }
            }
            return new Spool(directory, path, HexFormat.of().formatHex(digest.digest()), size, fileId, cleanup);
        } catch (ServiceException e) { cleanup.cleanup(path, directory, fileId); throw e; }
        catch (Exception e) { cleanup.cleanup(path, directory, fileId); throw new ServiceException(MessageUtils.message("payment.reconciliation.upload.file.receiveFailed")); }
    }

    private Path createPrivateDirectory() throws IOException {
        Path dir = Files.createTempDirectory("payment-reconciliation-"); secure(dir, true); return dir;
    }

    private void secure(Path path, boolean directory) throws IOException {
        PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null) {
            posix.setPermissions(PosixFilePermissions.fromString(directory ? "rwx------" : "rw-------")); return;
        }
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl == null) throw new IOException("No secure file attribute view");
        UserPrincipal owner = Files.getOwner(path);
        Set<AclEntryPermission> permissions = EnumSet.allOf(AclEntryPermission.class);
        AclEntry entry = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner)
            .setPermissions(permissions).build();
        acl.setAcl(List.of(entry));
    }

    public record Spool(Path directory, Path path, String digest, long size, String fileId,
                        PaymentReconciliationSpoolCleanup cleanup) implements AutoCloseable {
        @Override public void close() { cleanup.cleanup(path, directory, fileId); }
    }
}
