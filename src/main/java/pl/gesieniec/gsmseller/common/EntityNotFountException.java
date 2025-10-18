package pl.gesieniec.gsmseller.common;

public class EntityNotFountException extends RuntimeException{
    public EntityNotFountException(String message) {
        super(message);
    }
}
