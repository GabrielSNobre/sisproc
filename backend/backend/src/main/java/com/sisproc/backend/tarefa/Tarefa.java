package com.sisproc.backend.tarefa;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tarefa atribuída a um integrante de uma equipe.
 * Usa IDs escalares para não impor alterações às entidades preexistentes.
 */
@Entity
@Table(name = "sisproc_tarefa")
public class Tarefa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipe_id", nullable = false)
    private Long equipeId;

    @Column(name = "responsavel_id", nullable = false)
    private Long responsavelId;

    @Column(name = "criado_por_id", nullable = false)
    private Long criadoPorId;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(nullable = false, length = 12)
    private String prioridade = "MEDIA";

    @Column(name = "prazo")
    private LocalDate prazo;

    @Column(nullable = false, length = 20)
    private String status = "PENDENTE";

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    protected Tarefa() { }

    public Tarefa(Long equipeId, Long responsavelId, Long criadoPorId,
                  String titulo, String descricao, String prioridade, LocalDate prazo) {
        this.equipeId = equipeId;
        this.responsavelId = responsavelId;
        this.criadoPorId = criadoPorId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.prioridade = prioridade;
        this.prazo = prazo;
        this.status = "PENDENTE";
    }

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
        if (status == null || status.isBlank()) status = "PENDENTE";
        atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    void aoAtualizar() { atualizadoEm = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getEquipeId() { return equipeId; }
    public Long getResponsavelId() { return responsavelId; }
    public Long getCriadoPorId() { return criadoPorId; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public String getPrioridade() { return prioridade; }
    public LocalDate getPrazo() { return prazo; }
    public String getStatus() { return status; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setStatus(String status) { this.status = status; }
}
