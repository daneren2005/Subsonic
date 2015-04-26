package github.daneren2005.dsub.domain;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public enum RepeatMode {
    OFF {
        @Override
        public RepeatMode next() {
            return ALL;
        }
    },
    ALL {
        @Override
        public RepeatMode next() {
            return SINGLE;
        }
    },
    SINGLE {
        @Override
        public RepeatMode next() {
            return OFF;
        }
    };

    public abstract RepeatMode next();
}
