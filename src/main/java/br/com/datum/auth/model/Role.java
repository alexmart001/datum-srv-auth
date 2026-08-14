package br.com.datum.auth.model;

/**
 * Perfis de acesso suportados.
 *
 * USER  -> apenas operações de consulta nos serviços protegidos.
 * ADMIN -> consulta, criação, alteração e exclusão.
 */
public enum Role {
    USER,
    ADMIN
}
