package org.lml.thirdService.knowledgeService.utils;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.ArrayList;
import java.util.List;

public class MultiContentRetriever implements ContentRetriever {

    private final List<ContentRetriever> retrievers;

    public MultiContentRetriever(List<ContentRetriever> retrievers) {
        this.retrievers = retrievers;
    }

    @Override
    public List<Content> retrieve(Query query) {
        List<Content> results = new ArrayList<>();
        for (ContentRetriever retriever : retrievers) {
            List<Content> retrieved = retriever.retrieve(query);
            if (retrieved != null) {
                results.addAll(retrieved);
            }
        }
        return results;
    }
}
