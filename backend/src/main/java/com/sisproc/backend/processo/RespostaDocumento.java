package com.sisproc.backend.processo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sisproc_resposta_documento", indexes = @Index(name="idx_resposta_documento",columnList="documento_id"))
public class RespostaDocumento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="documento_id",nullable=false) private Long documentoId;
    @Column(name="autor_id",nullable=false) private Long autorId;
    @Column(nullable=false,length=8000) private String conteudo;
    @Column(name="criado_em",nullable=false,updatable=false) private LocalDateTime criadoEm;
    protected RespostaDocumento() {}
    public RespostaDocumento(Long documentoId, Long autorId, String conteudo) {
        this.documentoId = documentoId; this.autorId = autorId; this.conteudo = conteudo;
    }
    @PrePersist void criarData() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getDocumentoId() { return documentoId; }
    public Long getAutorId() { return autorId; }
    public String getConteudo() { return conteudo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
