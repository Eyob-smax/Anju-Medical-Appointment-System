package com.anju.domain.property;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
