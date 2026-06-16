package com.entitykart.productservice.controller;

import com.entitykart.productservice.dto.ProductDTO;
import com.entitykart.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GraphQLProductController {

    private final ProductService productService;

    @QueryMapping
    public ProductDTO product(@Argument Long id) {
        return productService.getProduct(id);
    }
}
