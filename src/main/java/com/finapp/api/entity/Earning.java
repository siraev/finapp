package com.finapp.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name="earnings")
public class Earning {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name="period")
    private String period;

    @Column(name="year")
    private Long year;

    @Column(name="date")
    private LocalDate date;

    @Column(name="eps")
    private Float eps;

    public Earning() {
    }

    public Earning(Company company, String period, Long year, Float eps) {
        this.company = company;
        this.period = period;
        this.year = year;
        this.eps = eps;
    }

    public Earning(Company company, String period, LocalDate date, Float eps) {
        this.company = company;
        this.period = period;
        this.date = date;
        this.eps = eps;
    }

}
