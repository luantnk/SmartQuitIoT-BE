package com.smartquit.smartquitiot.util.agora;

public interface PackableEx extends Packable{
    void unmarshal(ByteBuf in);
}
