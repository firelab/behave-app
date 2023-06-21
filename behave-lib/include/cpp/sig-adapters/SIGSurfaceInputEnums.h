#ifndef SIGSURFACEINPUTENUMS_H
#define SIGSURFACEINPUTENUMS_H

struct WindUpslopeAlignmentMode
{
  enum WindUpslopeAlignmentModeEnum
    {
      NotAligned,
      Aligned
    };
};

struct SurfaceRunInDirectionOf
{
  enum SurfaceRunInDirectionOfEnum
    {
      MaxSpread,
      DirectionOfInterest
    };
};

#endif // SURFACEINPUTENUMS_H
