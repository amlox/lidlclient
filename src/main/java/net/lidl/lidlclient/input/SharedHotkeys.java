package net.lidl.lidlclient.input;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;
import java.util.function.BooleanSupplier;

/** Independent, non-consuming hotkeys. No Minecraft KeyMapping registry or key-state writes. */
public final class SharedHotkeys {
    public record Key(int symbol,int scancode) {
        public static Key symbol(int value) { return new Key(value,-1); }
        public static Key scancode(int value) { return new Key(-1,value); }
        boolean matches(int key,int scan) { return symbol==-1?scancode==scan:symbol==key; }
    }
    private record Binding(List<Key> keys,BooleanSupplier active,LongConsumer onPress) { }
    private record Press(String owner,Binding binding,long window) { }
    private final Map<String,Binding> bindings=new LinkedHashMap<>();
    private final ArrayDeque<Press> pending=new ArrayDeque<>();
    public void bind(String owner,List<Key> keys,LongConsumer onPress) {
        bind(owner,keys,() -> true,onPress);
    }
    public void bind(String owner,List<Key> keys,BooleanSupplier active,LongConsumer onPress) {
        bindings.put(owner,new Binding(List.copyOf(keys),active,onPress));
    }
    public void unbind(String owner) { bindings.remove(owner); }
    /** Called by the native callback; defer actions until normal keyboard dispatch has returned. */
    public void observe(long window,int key,int scancode,int action) {
        if (action!=1) return; // GLFW_PRESS only; repeats and releases still go to the original callback.
        for (var entry:bindings.entrySet()) {
            if (pending.size()>=64) return;
            Binding binding=entry.getValue();
            if (!binding.active().getAsBoolean()) continue;
            for (Key input:binding.keys()) {
                if (!input.matches(key,scancode)) continue;
                pending.addLast(new Press(entry.getKey(),binding,window));
                break;
            }
        }
    }
    public void dispatch() {
        // Bound the pass even if a listener queues another event.
        int count=pending.size();
        for (int i=0;i<count;i++) {
            Press press=pending.removeFirst();
            if (bindings.get(press.owner())==press.binding()) press.binding().onPress().accept(press.window());
        }
    }
}
