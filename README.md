## Monitor Warehouse 

This contains implementations of various monitors at various granularities
to impress upon myself the lunacy of writing monitors by hand!
Example monitors largely come from [the expresso paper](http://kferles.github.io/docs/publications/PLDI-18.pdf).

In the `src` directory, the following monitors are implemented:

- `ReadersWriters.java` holds standard read-write monitors which make sure readers don't enter during a write and writers don't enter during a read.
