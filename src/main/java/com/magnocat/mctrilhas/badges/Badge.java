package com.magnocat.mctrilhas.badges;

/**
 * Representa uma insígnia com todas as suas propriedades carregadas da configuração.
 * Esta classe é imutável e usa a sintaxe de 'record' do Java para concisão.
 */
public record Badge(String id, String name, String description, BadgeType type, double requirement, String icon) {
}