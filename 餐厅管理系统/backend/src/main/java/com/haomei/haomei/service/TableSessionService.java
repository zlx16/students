package com.haomei.haomei.service;

import com.haomei.haomei.dto.TableResolveResponse;
import com.haomei.haomei.dto.TableSessionResponse;
import com.haomei.haomei.entity.OrderStatus;
import com.haomei.haomei.entity.TableSession;
import com.haomei.haomei.repository.OrderRepository;
import com.haomei.haomei.repository.TableSessionRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Base64;
import java.util.EnumSet;

@Service
public class TableSessionService {
    private final TableSessionRepository repo;
    private final OrderRepository orderRepository;
    private final String configuredBaseUrl;

    public TableSessionService(
            TableSessionRepository repo,
            OrderRepository orderRepository,
            @Value("${app.site.baseUrl:}") String configuredBaseUrl
    ) {
        this.repo = repo;
        this.orderRepository = orderRepository;
        this.configuredBaseUrl = configuredBaseUrl;
    }

    public TableResolveResponse resolve(String token) {
        TableSession t = repo.findByToken(token).orElseThrow(() -> new IllegalArgumentException("无效二维码/桌台token"));
        return new TableResolveResponse(t.getTableNo());
    }

    public List<TableSessionResponse> listAll(String frontendBaseUrl) {
        String effectiveBase = resolveBaseUrl(frontendBaseUrl);
        List<TableSession> tables = repo.findAll().stream()
                .sorted(Comparator.comparingInt(TableSession::getTableNo))
                .toList();

        for (TableSession t : tables) {
            String targetUrl = effectiveBase + "/?t=" + t.getToken();
            t.setQrDataUrl(generateQrDataUrl(targetUrl));
        }
        repo.saveAll(tables);

        return tables.stream()
                .map(t -> {
                    String url = effectiveBase + "/?t=" + t.getToken();
                    long activeOrderCount = orderRepository.countByTableNoAndStatusIn(
                            t.getTableNo(),
                            EnumSet.of(OrderStatus.WAIT_PAY, OrderStatus.PENDING, OrderStatus.COOKING)
                    );
                    boolean inUse = activeOrderCount > 0;
                    return new TableSessionResponse(
                            t.getTableNo(),
                            t.getToken(),
                            url,
                            t.getQrDataUrl(),
                            inUse,
                            activeOrderCount,
                            inUse ? "正在用餐" : "空闲"
                    );
                })
                .toList();
    }

    /** 重新生成全部二维码（当 baseUrl 变更时调用） */
    public void regenerateAllQrCodes(String frontendBaseUrl) {
        String effectiveBase = resolveBaseUrl(frontendBaseUrl);
        List<TableSession> tables = repo.findAll();
        for (TableSession t : tables) {
            String targetUrl = effectiveBase + "/?t=" + t.getToken();
            t.setQrDataUrl(generateQrDataUrl(targetUrl));
        }
        repo.saveAll(tables);
    }

    private String resolveBaseUrl(String frontendBaseUrl) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return normalizeUrl(configuredBaseUrl);
        }
        if (frontendBaseUrl != null && !frontendBaseUrl.isBlank()) {
            return normalizeUrl(frontendBaseUrl);
        }
        return "http://localhost:5173";
    }

    private String normalizeUrl(String url) {
        String u = url.trim();
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private String generateQrDataUrl(String text) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | java.io.IOException e) {
            throw new IllegalStateException("生成二维码失败: " + text, e);
        }
    }
}
