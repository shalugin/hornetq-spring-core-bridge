package ru.ashalugin.config.slave;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class SlaveMessage implements Serializable {
    private long id;
    private String message;
    private String instanceCode;
}