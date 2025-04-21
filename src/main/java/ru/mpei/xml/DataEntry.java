package ru.mpei.xml;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataEntry {
    private String type;
    private String value;

}
