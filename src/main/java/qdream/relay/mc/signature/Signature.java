package qdream.relay.mc.signature;

import java.util.List;

public interface Signature<I, O> {

    public List<I> getInputs();

    public List<O> getOutputs();

    public int inputCount();

    public int outputCount();
}
