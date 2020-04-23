package com.finapp.api.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name="ratios")
public class Ratio {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="pe")
    private Float pe;

    @Column(name="pe_ttm")
    private Float ttmPe;

    @Column(name="pb")
    private Float pb;

    @Column(name="bps")
    private Float bps;

    @Column(name="roe")
    private Float roe;

    @Column(name="rote")
    private Float rote;

    @Column(name="roa")
    private Float roa;

    @Column(name="roi")
    private Float roi;

    @Column(name="cur_ratio")
    private Float currentRatio;

    @Column(name="ltdebt_to_cap")
    private Float ltDebtToCapital;

    @Column(name="debt_to_equity")
    private Float debtToEquity;

}
