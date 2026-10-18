package com.sisproc.backend.processo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sisproc_documento_processo", indexes = {
    @Index(name = "idx_documento_tarefa", columnList = "tarefa_id"),
    @Index(name = "idx_documento_destino", columnList = "equipe_destino_id")
})
public class DocumentoProcesso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tarefa_id", nullable = false) private Long tarefaId;
    @Column(name = "equipe_origem_id", nullable = false) private Long equipeOrigemId;
    @Column(name = "equipe_destino_id") private Long equipeDestinoId;
    @Column(name = "autor_id", nullable = false) private Long autorId;
    @Column(nullable = false, length = 30) private String tipo;
    @Column(nullable = false, length = 160) private String assunto;
    @Column(nullable = false, length = 8000) private String conteudo;
    @Column(name = "criado_em", nullable = false, updatable = false) private LocalDateTime criadoEm;
    protected DocumentoProcesso() {}
    public DocumentoProcesso(Long tarefaId, Long equipeOrigemId, Long equipeDestinoId, Long autorId,
                             String tipo, String assunto, String conteudo) {
        this.tarefaId = tarefaId; this.equipeOrigemId = equipeOrigemId;
        this.equipeDestinoId = equipeDestinoId; this.autorId = autorId;
        this.tipo = tipo; this.assunto = assunto; this.conteudo = conteudo;
    }
    @PrePersist void criarData() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getTarefaId() { return tarefaId; }
    public Long getEquipeOrigemId() { return equipeOrigemId; }
    public Long getEquipeDestinoId() { return equipeDestinoId; }
    public Long getAutorId() { return autorId; }
    public String getTipo() { return tipo; }
    public String getAssunto() { return assunto; }
    public String getConteudo() { return conteudo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
