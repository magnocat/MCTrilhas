package com.magnocat.mctrilhas.ctf;

public enum FlagState {
    AT_BASE,  // A bandeira está na base.
    CARRIED,  // Um inimigo está carregando a bandeira.
    DROPPED   // A bandeira está no chão após o portador morrer.
}