package com.example.gsm.entity.repository;

import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;

public class RegexFindAggregationExpression implements AggregationExpression {
    private final String inputField;
    private final String regex;

    private RegexFindAggregationExpression(String inputField, String regex) {
        this.inputField = inputField;
        this.regex = regex;
    }

    public static RegexFindAggregationExpression regexFind(String inputField, String regex) {
        return new RegexFindAggregationExpression(inputField, regex);
    }

    @Override
    public Document toDocument(AggregationOperationContext context) {
        return new Document("$regexFind",
                new Document("input", "$" + inputField)
                        .append("regex", regex));
    }
}
