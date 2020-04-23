package com.finapp.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Optional;

@Data
@Entity
@Table(name="stocks")
public class Stock {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="symbol")
    private String symbol;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "company_id")
    private Company company;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "ratio_id")
    private Ratio ratio;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "extremum_id")
    private Extremum extremum;

    public Optional<Ratio> getRatio() {
        return Optional.ofNullable(this.ratio);
    }

    public Optional<Extremum> getExtremum() {
        return Optional.ofNullable(this.extremum);
    }

}
