package ru.ashalugin.config.master;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public class MasterMessage implements Serializable {
    private long id;
    private String message;
}