package com.sisproc.backend.comum;

import java.time.LocalDateTime;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho
) {

    public static ApiError criar(
            int status,
            String erro,
            String mensagem,
            String caminho) {

        return new ApiError(
                LocalDateTime.now(),
                status,
                erro,
                mensagem,
                caminho
        );
    }
}
