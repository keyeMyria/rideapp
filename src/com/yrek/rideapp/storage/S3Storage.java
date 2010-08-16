package com.yrek.rideapp.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3Storage implements Storage {
    private static final Logger LOG = Logger.getLogger(S3Storage.class.getName());

    private final AmazonS3 amazonS3;
    private final String bucketName;
    private final String prefix;

    public S3Storage(AmazonS3 amazonS3, String bucketName, String prefix) {
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.prefix = prefix;
    }

    @Override
    public String[] listFiles(String dir) {
        ArrayList<String> files = new ArrayList<String>();
        ObjectListing objectListing = amazonS3.listObjects(bucketName, prefix + dir);
        assert !objectListing.isTruncated();
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries())
            files.add(objectSummary.getKey().substring(prefix.length()+dir.length()));
        return files.toArray(new String[files.size()]);
    }

    @Override
    public byte[] readFile(String path) {
        InputStream in = null;
        try {
            in = amazonS3.getObject(bucketName, prefix + path).getObjectContent();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) >= 0)
                bytes.write(buffer, 0, count);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }
        }
    }

    @Override
    public void deleteFile(String path) {
        amazonS3.deleteObject(bucketName, prefix + path);
    }

    @Override
    public void writeFile(String path, byte[] content) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(content.length);
        amazonS3.putObject(bucketName, prefix + path, new ByteArrayInputStream(content), objectMetadata);
    }
}
