package com.story.sonder.model.loss;

import android.util.Pair;

import com.story.sonder.model.Tensor;

public class BCELoss implements ILoss {
    @Override
    public Pair<Double, Object> forward(Tensor input, Tensor target) {
        // TODO: Compute the loss and return with input to back-prop
        return null;
    }

    @Override
    public Tensor backward(double gradInput, Object backInput) {
        // TODO: Pass the gradient backwards
        return null;
    }

    @Override
    public String toString() {
        return "BCELoss()";
    }
}