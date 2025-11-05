package io.akeyless.cloudid;

import io.akeyless.cloudid.aws.AwsCredentialResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AwsCredentialResolverProfileTest {
    private File tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("aws-profile-test").toFile();
        File credentials = new File(tempDir, "credentials");
        try (FileWriter fw = new FileWriter(credentials)) {
            fw.write("[default]\n");
            fw.write("aws_access_key_id=TESTKEY_FROM_PROFILE\n");
            fw.write("aws_secret_access_key=TESTSECRET_FROM_PROFILE\n");
            fw.write("aws_session_token=TESTSESSION_FROM_PROFILE\n");
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (tempDir != null) deleteRecursively(tempDir);
    }

    @Test
    public void resolvesFromDefaultProfileWhenNoEnv() throws Exception {
        AwsCredentialResolver resolver = new AwsCredentialResolver(
                new io.akeyless.cloudid.http.JdkHttpTransport(),
                "default",
                new File(tempDir, "credentials").getAbsolutePath(),
                null,
                true // ignore env
        );
        AwsCredentialResolver.AwsCredentials creds = resolver.resolve();
        assertEquals("TESTKEY_FROM_PROFILE", creds.accessKeyId);
        assertEquals("TESTSECRET_FROM_PROFILE", creds.secretAccessKey);
        assertEquals("TESTSESSION_FROM_PROFILE", creds.sessionToken);
    }

    private static void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        f.delete();
    }

    // No env mutation needed – constructor overrides are used
}


