package com.finapp.api.repository;

import com.finapp.api.entity.Company;
import com.finapp.api.entity.Earning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EarningRepository extends JpaRepository<Earning, Long> {

    List<Earning> findAllByCompanyAndPeriod(Company company, String period);

    List<Earning> findFirst3ByCompanyAndPeriodOrderByYearDesc(Company company, String period);

    List<Earning> findFirst4ByCompanyAndPeriodOrderByDateDesc(Company company, String period);

}
