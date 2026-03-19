package com.haomei.haomei.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "table_session")
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer tableNo;

    @Column(nullable = false, unique = true)
    private String token;

    @Lob
    @Column(name = "qr_data_url", columnDefinition = "TEXT")
    private String qrDataUrl;

    public Long getId() {
        return id;
    }

    public Integer getTableNo() {
        return tableNo;
    }

    public void setTableNo(Integer tableNo) {
        this.tableNo = tableNo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getQrDataUrl() {
        return qrDataUrl;
    }

    public void setQrDataUrl(String qrDataUrl) {
        this.qrDataUrl = qrDataUrl;
    }
}

