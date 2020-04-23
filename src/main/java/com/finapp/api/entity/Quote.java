package com.finapp.api.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name="quotes")
public class Quote implements Comparable<Quote> {

    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Column(name="date")
    private LocalDate date;

    @Column(name="open")
    private float open;

    @Column(name="high")
    private float high;

    @Column(name="low")
    private float low;

    @Column(name="close")
    private float close;

    @Column(name="volume")
    private long volume;

    @Override
    public int compareTo(Quote quote) {
        return this.date.compareTo(quote.date);
    }

}
