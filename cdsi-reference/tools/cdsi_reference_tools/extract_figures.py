"""Crops each figure listed in the specification's own List of Figures and
Tables (see toc.py) directly from the rendered page, rather than pulling
embedded image objects - CDSi's process-model figures are a mix of
embedded raster images and vector-drawn flowchart shapes, and a page-region
render (page.get_pixmap(clip=...)) handles both uniformly at full fidelity
without caring which one a given figure happens to be.

A figure's vertical extent is located by finding its own caption text and
the next caption/heading below it; the caller is expected to have located
the previous caption/heading's bottom edge as the top bound (this module
only needs the page and this figure's own caption position).
"""

from dataclasses import dataclass

import pymupdf as fitz


@dataclass
class ExtractedFigure:
    number: str
    page_index: int
    png_bytes: bytes | None
    warnings: list[str]


def caption_rect(page: "fitz.Page", figure_number: str) -> "fitz.Rect | None":
    hits = page.search_for(f"FIGURE {figure_number}", quads=False)
    if not hits:
        return None
    # If the caption text itself spans more than one search hit (rare),
    # take the topmost.
    return min(hits, key=lambda r: r.y0)


def extract_figure(
    doc: "fitz.Document",
    figure_number: str,
    page_index: int,
    top_bound: float = 0.0,
    bottom_bound: float | None = None,
    dpi: int = 200,
) -> ExtractedFigure:
    """Crops from `top_bound` down to this figure's own caption text
    (captions sit BELOW CDSi's figures, so the image content is between
    the previous boundary and this figure's caption, inclusive of the
    caption itself so the figure identifier stays visible in the crop)."""
    warnings: list[str] = []
    page = doc[page_index]
    caption = caption_rect(page, figure_number)
    if caption is None:
        warnings.append(
            f"Could not locate the caption text 'FIGURE {figure_number}' on page {page_index}; "
            "cannot determine a reliable crop region."
        )
        return ExtractedFigure(number=figure_number, page_index=page_index, png_bytes=None, warnings=warnings)

    bottom = caption.y1 + 4  # a few points of padding below the caption text
    if bottom_bound is not None:
        bottom = min(bottom, bottom_bound)
    clip = fitz.Rect(0, max(top_bound, 0), page.rect.width, bottom)
    if clip.height <= 0:
        warnings.append(f"Computed crop region for FIGURE {figure_number} has non-positive height: {clip}")
        return ExtractedFigure(number=figure_number, page_index=page_index, png_bytes=None, warnings=warnings)

    pixmap = page.get_pixmap(clip=clip, dpi=dpi)
    return ExtractedFigure(number=figure_number, page_index=page_index, png_bytes=pixmap.tobytes("png"), warnings=warnings)
