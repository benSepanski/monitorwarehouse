## Monitor Warehouse 

This contains implementations of various monitors at various granularities
to impress upon myself the lunacy of writing monitors by hand!
Example monitors largely come from [the expresso paper](http://kferles.github.io/docs/publications/PLDI-18.pdf).

In the `src` directory, the following monitors are implemented:

- `ReadersWritersMonitor.java` holds standard read-write monitors which make sure readers don't enter during a write and writers don't enter during a read.
- `BoundedBufferMonitor.java` holds a monitor which blocks threads wanting to write on a buffer until there is sufficient space and blocks threads trying to read `n` items from the buffer until there are `n` items on the buffer
- `SleepingBarberMonitor.java` holds a monitor which
    - lets a barber wait until a customer wakes them up or one is in the waiting room, then cuts their hair
    - lets customers come into the shop, wake up the barber if necessary or take a spot in the waiting room if the barber is cutting hair (and there is a spot available, otherwise leave without a haircut)
