package com.mbeebe.docket.images;

import org.springframework.data.repository.Repository;

import java.util.Optional;

interface ImageRepository extends Repository<Image, Long> {

    Image save(Image image);

    Optional<Image> findById(Long id);
}
