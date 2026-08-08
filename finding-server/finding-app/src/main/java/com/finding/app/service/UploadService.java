package com.finding.app.service;

public interface UploadService {

    String uploadImage(byte[] data, String originalFilename, String contentType);
}
