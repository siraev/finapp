package com.finapp.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name="extremums")
public class Extremum {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="down_from_max")
    private Float downFromMax;

    @Column(name="up_from_min_2018")
    private Float upFromMin2018;

    @Column(name="up_from_min_2016")
    private Float upFromMin2016;

    @Column(name="up_from_min_2008")
    private Float upFromMin2008;

    @Column(name="up_from_min_2000")
    private Float upFromMin2000;

}
