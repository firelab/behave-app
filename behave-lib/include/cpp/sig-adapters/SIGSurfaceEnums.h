#ifndef SIGSURFACEENUMS_H
#define SIGSURFACEENUMS_H

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

#endif // SURFACEENUMS_H
