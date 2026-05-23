package com.auth.totp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Component
public class QrCodeGenerator {

    public String generateQrCodeBase64(String otpAuthUrl) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    otpAuthUrl,
                    BarcodeFormat.QR_CODE,
                    250, 250
            );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("QR code generation failed", e);
        }
    }
}
